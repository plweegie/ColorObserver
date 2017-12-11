from rest_framework import generics
from rest_framework.decorators import api_view
from rest_framework.parsers import JSONParser
from rest_framework.response import Response
from rest_framework.reverse import reverse
from rest_framework.views import APIView
from rest_framework import status

from .models import ColorMeasurement
from .serializers import ColorSerializer


@api_view(['GET'])
def api_root(request, format=None):
    return Response({'colors': reverse('color-list', request=request, format=format)})
    

class ColorList(APIView):

    parser_classes = (JSONParser,)

    def get(self, request, format=None):
        colors = ColorMeasurement.objects.all()
        serializer = ColorSerializer(colors, many=True)
        return Response(serializer.data)

    def post(self, request, format=None):
        serializer = ColorSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
    
    
class ColorDetail(generics.RetrieveUpdateDestroyAPIView):
    queryset = ColorMeasurement.objects.all()
    serializer_class = ColorSerializer
