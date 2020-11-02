import sys,os
import torch
from torchvision import transforms,models

from PIL import Image
#from efficientnet_pytorch import EfficientNet
import pretrainedmodels
print(pretrainedmodels.model_names)

def get_model(model_name, model_class):
    if model_name.startswith('efficientnet'):
        #return torch.hub.load('rwightman/gen-efficientnet-pytorch', 'efficientnet_b0', pretrained=True)
        return EfficientNet.from_pretrained(model_name)
    elif model_name.startswith('pretrained_'):
        print(model_class)
        return model_class(num_classes=1000, pretrained='imagenet')
    else:
        return model_class(pretrained=True)
if __name__ == '__main__':
    output_model_dir='neural_models/app/src/main/assets'
    INPUT_SIZE=224
    example = torch.rand(1, 3, INPUT_SIZE, INPUT_SIZE)
    all_models={'mobilenet_v2':models.mobilenet_v2,
        'mnasnet':models.mnasnet1_0, 'resnet18':models.resnet18,
        'pretrained_nasnetmobile':pretrainedmodels.nasnetamobile,'pretrained_squeezenet':pretrainedmodels.squeezenet1_1#,'efficientnet-b0':None
        }
    for model_name in all_models:
        filename=os.path.join(output_model_dir,model_name)
        if not os.path.exists(filename):
            model = get_model(model_name,all_models[model_name])
            #print(model)
            model.eval()
            quantized_model = torch.quantization.quantize_dynamic(model, dtype=torch.qint8)
            if True:
                traced_script_module = torch.jit.trace(model, example)
                traced_script_module.save(filename+'.pt')
                traced_script_module = torch.jit.trace(quantized_model, example)
                traced_script_module.save(filename+'_quant.pt')
            else:
                transform = transforms.Compose([
                    transforms.Resize(INPUT_SIZE),
                    #transforms.CenterCrop(INPUT_SIZE),
                    transforms.ToTensor(),
                    transforms.Normalize(
                        mean=[0.485, 0.456, 0.406],
                        std=[0.229, 0.224, 0.225])
                        ])

                # image
                img = Image.open('D:/src_code/mobile/MADE-mobile-image-processing/lesson1/src/test_pytorch/mobile_app/app/src/main/assets/cat.2013.jpg')
                img = transform(img)
                img = torch.unsqueeze(img, 0)
                print(img.shape)

                model.eval()
                out = model(img)
                percents = torch.nn.functional.softmax(out, dim=1)[0] * 100
                print(percents.topk(5))
                print(out.topk(5))
